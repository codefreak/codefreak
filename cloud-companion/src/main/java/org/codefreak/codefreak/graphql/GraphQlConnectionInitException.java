package org.codefreak.codefreak.graphql;

import org.springframework.web.reactive.socket.CloseStatus;

public class GraphQlConnectionInitException extends RuntimeException {

  private final CloseStatus closeCode;

  public GraphQlConnectionInitException(
    String message,
    CloseStatus closeStatus
  ) {
    super(message);
    this.closeCode = closeStatus;
  }

  public CloseStatus getCloseCode() {
    return closeCode;
  }

  public static GraphQlConnectionInitException fromCode(
    int code,
    String reason
  ) {
    return new GraphQlConnectionInitException(
      reason,
      new CloseStatus(code, reason)
    );
  }
}
