package com.amazonaws.example.cmr;

import software.amazon.awscdk.App;

public class InfrastructureApp {
    public static void main(final String[] args) {
        App app = new App();

        new InfrastructureStack(app, "UnicornStoreStack");

        app.synth();
    }
}
