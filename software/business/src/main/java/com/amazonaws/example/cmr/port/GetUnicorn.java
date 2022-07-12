package com.amazonaws.example.cmr.port;

import com.amazonaws.example.cmr.domain.GetUnicornException;
import com.amazonaws.example.cmr.domain.Unicorn;

public interface GetUnicorn {

    Unicorn getUnicorn(String unicornId) throws GetUnicornException;

}
