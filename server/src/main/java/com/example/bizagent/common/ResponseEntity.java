
package com.example.bizagent.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseEntity<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ResponseEntity<T> success(T data) {
        return new ResponseEntity<>(200, "success", data);
    }

    public static <T> ResponseEntity<T> success(String message, T data) {
        return new ResponseEntity<>(200, message, data);
    }

    public static <T> ResponseEntity<T> error(int code, String message) {
        return new ResponseEntity<>(code, message, null);
    }

    public static <T> ResponseEntity<T> error(String message) {
        return new ResponseEntity<>(500, message, null);
    }
}
