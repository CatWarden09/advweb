package ru.catwarden.advweb.exception;

public class EntityNotFoundException extends RuntimeException{
    public EntityNotFoundException(Class<?> entityClass, Object id) {
        super(String.format("%s with id=%s not found",
                entityClass.getSimpleName(),
                id));
    }
}
