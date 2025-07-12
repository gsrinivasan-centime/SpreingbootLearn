package com.bookstore.userservice.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Converter
public class PhoneNumberConverter implements AttributeConverter<String, String> {

    private final StringEncryptor stringEncryptor;

    @Autowired
    public PhoneNumberConverter(@Qualifier("phoneNumberEncryptor") StringEncryptor stringEncryptor) {
        this.stringEncryptor = stringEncryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.trim().isEmpty()) {
            return null;
        }
        return stringEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return stringEncryptor.decrypt(dbData);
        } catch (Exception e) {
            // Log the error and return null or throw a custom exception
            // For now, we'll return null to handle decryption errors gracefully
            return null;
        }
    }
}
