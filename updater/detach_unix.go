//go:build !windows

package main

import (
	"os/exec"
)

// detach запускает дочерний процесс отдельно от апдейтера.
func detach(cmd *exec.Cmd) {
}
