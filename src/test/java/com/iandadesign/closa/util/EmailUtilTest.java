package com.iandadesign.closa.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailUtilTest {

    @Test
    void sendMail() {
        EmailUtil.sendMail("subject", "test");
    }
}