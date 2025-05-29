package com.redcat.tutorials.gitcloner;

public interface GitService {

    void cloneRepository(String url) throws CloneRepositoryException;
}
