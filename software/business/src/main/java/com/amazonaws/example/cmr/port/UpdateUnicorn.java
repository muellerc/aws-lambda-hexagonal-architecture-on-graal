package com.amazonaws.example.cmr.port;

import com.amazonaws.example.cmr.domain.Unicorn;
import com.amazonaws.example.cmr.domain.UpdateUnicornException;

public interface UpdateUnicorn {

    Unicorn updateUnicorn(Unicorn unicorn) throws UpdateUnicornException;
}
