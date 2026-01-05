package com.socialmovieclub.core.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Sadece null olmayan alanları JSON'a basar (Temiz JSON)
public class RestResponse<T> {

    private T data;
    private Map<String, String> validationErrors; // Validation hataları için özel alan
    private String message;
    private boolean success;
    private int status;
    private LocalDateTime responseDate;

    // Başarı Constructor'ı
    public RestResponse(T data, String message, boolean success, int status) {
        this.data = data;
        this.message = message;
        this.success = success;
        this.status = status; //HttpStatus yerine int kod dönmek frontend için daha kolaydır
        this.responseDate = LocalDateTime.now();
    }

    // Hata Constructor'ı (Validation için)
    public RestResponse(Map<String, String> validationErrors, String message, int status) {
        this.validationErrors = validationErrors;
        this.message = message;
        this.success = false;
        this.status = status;
        this.responseDate = LocalDateTime.now();
    }

    // --- Statik Yardımcı Metotlar ---
    public static <T> RestResponse<T> success(T data) {
        return new RestResponse<>(data, "Operation successful", true, HttpStatus.OK.value());
    }

    // Hem data hem mesaj ile başarı (Dil desteği için bunu kullanacağız)
    public static <T> RestResponse<T> success(T data, String message) {
        return new RestResponse<>(data, message, true, HttpStatus.OK.value());
    }

    // Validation Hatası için yeni statik metot
    public static <T> RestResponse<T> validationError(Map<String, String> errors) {
        return new RestResponse<>(errors, "Validation Failed", HttpStatus.BAD_REQUEST.value());
    }

    public static <T> RestResponse<T> error(String message, HttpStatus status) {
        RestResponse<T> response = new RestResponse<>();
        response.setMessage(message);
        response.setSuccess(false);
        response.setStatus(status.value());
        response.setResponseDate(LocalDateTime.now());
        return response;
    }
}