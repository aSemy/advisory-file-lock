package socket

enum class Response {
  READ_GRANTED,
  READ_DENIED,
  WRITE_GRANTED,
  WRITE_DENIED,
  RELEASED,
  UNKNOWN_COMMAND,
}
