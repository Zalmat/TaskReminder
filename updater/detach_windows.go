//go:build windows

package main

import (
	"os/exec"
	"syscall"
)

// detach запускает дочерний процесс отдельно от апдейтера.
func detach(cmd *exec.Cmd) {
	const (
		detachedProcess       = 0x00000008
		createNewProcessGroup = 0x00000200
	)
	cmd.SysProcAttr = &syscall.SysProcAttr{
		CreationFlags: detachedProcess | createNewProcessGroup,
	}
}
