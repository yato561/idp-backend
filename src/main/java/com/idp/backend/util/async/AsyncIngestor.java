package com.idp.backend.util.async;

public interface AsyncIngestor<T> {
    void submit(T payload);
}
