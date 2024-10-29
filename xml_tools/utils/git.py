import subprocess
from pathlib import Path
from typing import Tuple
import logging

logger = logging.getLogger("xml_tools")


class GitClient:
    """
    Handler for Git operations on a repository.

    This class provides methods to perform Git operations on a specified repository path.

    Attributes:
        repo_path (Path): Path to the Git repository
    """

    def __init__(self, repo_path: Path) -> None:
        """
        Initialize GitClient with repository path.

        Args:
            repo_path (Path): Path to the Git repository

        Returns:
            None
        """
        self.repo_path = repo_path

    def run_command(self, command: list) -> Tuple[int, str, str]:
        """
        Execute a Git command and return its result.

        Args:
            command (list): List of command components (e.g., ["git", "pull"])

        Returns:
            Tuple[int, str, str]: A tuple containing:
                - Return code (0 for success)
                - Command output (stdout)
                - Error output (stderr)

        Note:
            Commands are executed in the repository directory specified during initialization.
        """
        try:
            result = subprocess.run(
                command,
                cwd=self.repo_path,
                capture_output=True,
                text=True
            )
            return result.returncode, result.stdout, result.stderr
        except subprocess.SubprocessError as e:
            logger.error(f"Git command failed: {e}")
            return 1, "", str(e)

    def sync_repository(self) -> bool:
        """
        Synchronize the repository with its remote.

        This method performs three operations in sequence:
        1. Switches to the 'dev' branch
        2. Fetches updates from remote
        3. Pulls changes

        Returns:
            bool: True if all operations succeeded, False otherwise

        Note:
            Logs success/failure of each operation through the logger.
        """
        operations = [
            (["git", "switch", "dev"], "checkout"),
            (["git", "fetch"], "fetch"),
            (["git", "pull"], "pull")
        ]

        for command, operation in operations:
            code, out, err = self.run_command(command)
            if code != 0:
                logger.error(f"Git {operation} failed: {err}")
                return False
            logger.info(f"Git {operation} successful")

        return True
