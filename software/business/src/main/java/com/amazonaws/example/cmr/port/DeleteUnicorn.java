package com.amazonaws.example.cmr.port;

import com.amazonaws.example.cmr.domain.DeleteUnicornException;

public interface DeleteUnicorn {

    void deleteUnicorn(String unicornId) throws DeleteUnicornException;
}
