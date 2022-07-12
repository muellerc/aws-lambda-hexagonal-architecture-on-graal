package com.amazonaws.example.cmr.port;

import com.amazonaws.example.cmr.domain.CreateUnicornException;
import com.amazonaws.example.cmr.domain.Unicorn;

public interface CreateUnicorn {

    Unicorn createUnicorn(Unicorn unicorn) throws CreateUnicornException;
}
