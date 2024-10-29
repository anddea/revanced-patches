class XMLToolsError(Exception):
    """Base exception for XML tools."""
    pass


class ConfigError(XMLToolsError):
    """Configuration related errors."""
    pass


class XMLProcessingError(XMLToolsError):
    """XML processing related errors."""
    pass
