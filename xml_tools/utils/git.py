"""Git commands."""

import logging
from pathlib import Path

import git

logger = logging.getLogger("xml_tools")


class GitClient:
    """Handler for Git operations on a repository using GitPython."""

    def __init__(self, repo_path: Path) -> None:
        """Initialize GitClient with repository path.

        Args:
            repo_path: Path to the Git repository.

        Raises:
            ValueError: If the provided path is not a valid Git repository.

        """
        self.repo_path = repo_path
        try:
            self.repo = git.Repo(repo_path, search_parent_directories=True)
        except git.InvalidGitRepositoryError as e:
            msg = f"Invalid Git repository at path: {repo_path}"
            raise ValueError(msg) from e

    def _handle_git_error(self, operation: str, exception: Exception) -> tuple[int, str, str]:
        """Handle GitPython exceptions.

        Args:
            operation: The name of the Git operation that failed.
            exception: The exception that was raised by GitPython.

        Returns:
            A tuple containing:
             - an error code (1),
             - an empty output string,
             - the exception message as error string.

        """
        logger.error("Git %s failed: %s", operation, exception)
        return 1, "", str(exception)

    def switch_branch(self, branch_name: str) -> tuple[int, str, str]:
        """Switches to the specified branch.

        Args:
            branch_name: The name of the branch to switch to.

        Returns:
            A tuple containing:
                - Return code (0 for success, 1 for failure)
                - Command output (success message or empty)
                - Error output (empty string on success, error message on failure)

        """
        try:
            self.repo.git.checkout(branch_name)
        except git.GitCommandError as e:
            return self._handle_git_error(f"checkout {branch_name}", e)
        else:
            return 0, f"Switched to branch {branch_name}", ""

    def fetch(self) -> tuple[int, str, str]:
        """Fetche updates from remote.

        Returns:
            A tuple containing:
                - Return code (0 for success, 1 for failure)
                - Command output (success message or empty)
                - Error output (empty string on success, error message on failure)

        """  # noqa: D401
        try:
            self.repo.remotes.origin.fetch()
        except git.GitCommandError as e:
            return self._handle_git_error("fetch", e)
        else:
            return 0, "Fetch successful", ""

    def pull(self) -> tuple[int, str, str]:
        """Pull changes from remote.

        Returns:
            A tuple containing:
                - Return code (0 for success, 1 for failure)
                - Command output (success message or empty)
                - Error output (empty string on success, error message on failure)

        """
        try:
            self.repo.remotes.origin.pull()
        except git.GitCommandError as e:
            return self._handle_git_error("pull", e)
        else:
            return 0, "Pull successful", ""

    def sync_repository(self) -> bool:
        """Synchronize the repository with its remote.

        This method performs three operations in sequence:
        1. Switches to the 'dev' branch
        2. Fetches updates from remote
        3. Pulls changes

        Returns:
            True if all operations succeeded, False otherwise.

        """
        operations = [
            (self.switch_branch, "dev"),
            (self.fetch,),
            (self.pull,),
        ]

        for operation, *args in operations:
            code, _, _ = operation(*args)
            if code != 0:
                return False
            logger.info("Git %s successful", operation.__name__)
        return True
