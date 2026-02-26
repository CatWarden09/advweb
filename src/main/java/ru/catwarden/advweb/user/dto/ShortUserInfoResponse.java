package ru.catwarden.advweb.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortUserInfoResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private Double rating;
    private String phone;

}
