// TaskReminder auto-updater: заменяет файлы уже запущенного приложения новой версией
// и перезапускает его. Запускается основным приложением из временной копии.
//
// Аргументы:
//
//	-pid <int>        PID приложения (используется как подсказка для ожидания)
//	-archive <path>   путь к скачанному архиву (zip или tar.gz)
//	-dir <path>       каталог установки (корень app-image)
//	-exe <name>       имя лаунчера (например, TaskReminder.exe); на Linux — имя в bin/
package main

import (
	"archive/tar"
	"archive/zip"
	"compress/gzip"
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"
)

var (
	logFile *os.File
	// Каталоги app-image, которые заменяются при обновлении.
	programDirs = []string{"app", "runtime", "bin", "lib", "updater"}
)

func logf(format string, args ...any) {
	line := time.Now().Format("2006-01-02 15:04:05 ") + fmt.Sprintf(format, args...) + "\n"
	if logFile != nil {
		_, _ = logFile.WriteString(line)
	}
}

func initLog() {
	path := filepath.Join(os.TempDir(), "TaskReminder-autoupdate.log")
	f, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
	if err == nil {
		logFile = f
	}
}

func main() {
	pid := flag.Int("pid", 0, "PID приложения")
	archive := flag.String("archive", "", "путь к скачанному архиву")
	dir := flag.String("dir", "", "каталог установки")
	exe := flag.String("exe", "", "имя лаунчера")
	flag.Parse()

	initLog()
	defer func() {
		if logFile != nil {
			_ = logFile.Close()
		}
	}()

	if *archive == "" || *dir == "" {
		logf("missing args: archive=%q dir=%q", *archive, *dir)
		os.Exit(2)
	}
	logf("start pid=%d arch=%q dir=%q exe=%q os=%s", *pid, *archive, *dir, *exe, runtime.GOOS)

	if err := run(*pid, *archive, *dir, *exe); err != nil {
		logf("ERROR: %v", err)
		os.Exit(1)
	}
	logf("done")
}

func run(pid int, archive, dir, exe string) error {
	cleanupStaleBackups(dir)

	tmp, err := os.MkdirTemp("", "tr-update-")
	if err != nil {
		return err
	}
	defer os.RemoveAll(tmp)

	if err := extract(archive, tmp); err != nil {
		return fmt.Errorf("extract: %w", err)
	}
	srcRoot, err := locateAppRoot(tmp)
	if err != nil {
		return err
	}
	items := collectItems(srcRoot, exe)
	if len(items) == 0 {
		return fmt.Errorf("в архиве нет файлов программы")
	}
	logf("replace items: %v", items)

	backup := filepath.Join(dir, fmt.Sprintf(".tr-backup-%d", time.Now().Unix()))
	if err := os.MkdirAll(backup, 0o755); err != nil {
		return err
	}

	if err := replace(dir, srcRoot, items, backup); err != nil {
		logf("replace failed, rolling back: %v", err)
		rollback(dir, items, backup)
		_ = os.RemoveAll(backup)
		if rerr := relaunch(dir, exe); rerr != nil {
			logf("relaunch old failed: %v", rerr)
		}
		return err
	}

	if err := relaunch(dir, exe); err != nil {
		logf("relaunch new failed: %v", err)
	}
	_ = os.RemoveAll(backup)
	_ = os.Remove(archive)
	return nil
}

// replace переносит старые файлы в backup (с ожиданием разблокировки) и копирует новые.
func replace(dir, srcRoot string, items []string, backup string) error {
	deadline := time.Now().Add(90 * time.Second)
	for _, item := range items {
		old := filepath.Join(dir, item)
		if _, err := os.Stat(old); err != nil {
			continue // файла нет — нечего переносить
		}
		for {
			err := os.Rename(old, filepath.Join(backup, item))
			if err == nil {
				break
			}
			if time.Now().After(deadline) {
				return fmt.Errorf("файл занят: %s: %w", item, err)
			}
			time.Sleep(300 * time.Millisecond)
		}
	}

	for _, item := range items {
		src := filepath.Join(srcRoot, item)
		dst := filepath.Join(dir, item)
		_ = os.RemoveAll(dst)
		if err := copyPath(src, dst); err != nil {
			return fmt.Errorf("установка %s: %w", item, err)
		}
	}
	return nil
}

func rollback(dir string, items []string, backup string) {
	for _, item := range items {
		_ = os.RemoveAll(filepath.Join(dir, item))
		old := filepath.Join(backup, item)
		if _, err := os.Stat(old); err == nil {
			_ = os.Rename(old, filepath.Join(dir, item))
		}
	}
}

func relaunch(dir, exe string) error {
	if exe == "" {
		return nil
	}
	path := filepath.Join(dir, exe)
	if runtime.GOOS != "windows" {
		candidate := filepath.Join(dir, "bin", exe)
		if _, err := os.Stat(candidate); err == nil {
			path = candidate
		}
	}
	if _, err := os.Stat(path); err != nil {
		return err
	}
	cmd := exec.Command(path)
	cmd.Dir = dir
	detach(cmd)
	return cmd.Start()
}

func collectItems(srcRoot, exe string) []string {
	var items []string
	for _, d := range programDirs {
		if isDir(filepath.Join(srcRoot, d)) {
			items = append(items, d)
		}
	}
	if exe != "" {
		if info, err := os.Stat(filepath.Join(srcRoot, exe)); err == nil && info.Mode().IsRegular() {
			items = append(items, exe)
		}
	}
	return items
}

func locateAppRoot(tmp string) (string, error) {
	entries, err := os.ReadDir(tmp)
	if err != nil {
		return "", err
	}
	var dirs []string
	files := 0
	for _, e := range entries {
		if e.IsDir() {
			dirs = append(dirs, e.Name())
		} else {
			files++
		}
	}
	if len(dirs) == 1 && files == 0 {
		return filepath.Join(tmp, dirs[0]), nil
	}
	return tmp, nil
}

func extract(archive, dest string) error {
	lower := strings.ToLower(archive)
	switch {
	case strings.HasSuffix(lower, ".zip"):
		return extractZip(archive, dest)
	case strings.HasSuffix(lower, ".tar.gz"), strings.HasSuffix(lower, ".tgz"):
		return extractTarGz(archive, dest)
	default:
		return fmt.Errorf("неизвестный формат архива: %s", archive)
	}
}

func extractZip(archive, dest string) error {
	reader, err := zip.OpenReader(archive)
	if err != nil {
		return err
	}
	defer reader.Close()

	for _, f := range reader.File {
		target, err := safeJoin(dest, f.Name)
		if err != nil {
			return err
		}
		if f.FileInfo().IsDir() {
			if err := os.MkdirAll(target, 0o755); err != nil {
				return err
			}
			continue
		}
		if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
			return err
		}
		src, err := f.Open()
		if err != nil {
			return err
		}
		if err := writeFile(target, src, f.Mode()); err != nil {
			src.Close()
			return err
		}
		src.Close()
	}
	return nil
}

func extractTarGz(archive, dest string) error {
	file, err := os.Open(archive)
	if err != nil {
		return err
	}
	defer file.Close()

	gz, err := gzip.NewReader(file)
	if err != nil {
		return err
	}
	defer gz.Close()

	tr := tar.NewReader(gz)
	for {
		header, err := tr.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
		target, err := safeJoin(dest, header.Name)
		if err != nil {
			return err
		}
		switch header.Typeflag {
		case tar.TypeDir:
			if err := os.MkdirAll(target, 0o755); err != nil {
				return err
			}
		case tar.TypeReg:
			if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
				return err
			}
			if err := writeFile(target, tr, os.FileMode(header.Mode)); err != nil {
				return err
			}
		}
	}
	return nil
}

func writeFile(target string, src io.Reader, mode os.FileMode) error {
	out, err := os.OpenFile(target, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, mode.Perm())
	if err != nil {
		return err
	}
	defer out.Close()
	_, err = io.Copy(out, src)
	return err
}

func copyPath(src, dst string) error {
	info, err := os.Stat(src)
	if err != nil {
		return err
	}
	if info.IsDir() {
		if err := os.MkdirAll(dst, info.Mode().Perm()); err != nil {
			return err
		}
		entries, err := os.ReadDir(src)
		if err != nil {
			return err
		}
		for _, e := range entries {
			if err := copyPath(filepath.Join(src, e.Name()), filepath.Join(dst, e.Name())); err != nil {
				return err
			}
		}
		return nil
	}
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()
	return writeFile(dst, in, info.Mode())
}

func safeJoin(base, name string) (string, error) {
	clean := filepath.Clean(name)
	if filepath.IsAbs(clean) || strings.HasPrefix(clean, "..") {
		return "", fmt.Errorf("небезопасный путь в архиве: %s", name)
	}
	target := filepath.Join(base, clean)
	if !strings.HasPrefix(target, filepath.Clean(base)+string(os.PathSeparator)) && target != filepath.Clean(base) {
		return "", fmt.Errorf("небезопасный путь в архиве: %s", name)
	}
	return target, nil
}

func isDir(path string) bool {
	info, err := os.Stat(path)
	return err == nil && info.IsDir()
}

func cleanupStaleBackups(dir string) {
	entries, err := os.ReadDir(dir)
	if err != nil {
		return
	}
	for _, e := range entries {
		if e.IsDir() && strings.HasPrefix(e.Name(), ".tr-backup-") {
			_ = os.RemoveAll(filepath.Join(dir, e.Name()))
		}
	}
}
