package dev.ambience.util;

public final class BuildConfig {
   public static final String NAME = "Ambience";
   public static final String GROUP = "dev.ambience";
   public static final String VERSION = "1.0.0";
   public static final String BUILD_TIME = "09/04/2026 19:26";
   public static final boolean USING_GIT = true;
   public static final String HASH = "de2a0eb";
   public static final String BRANCH = "main";
   // public static final String AUTH_SERVER = "https://auth.ambienceclient.com";  // original
   public static final String AUTH_SERVER = System.getenv("AMBIENCE_AUTH_SERVER") != null
       ? System.getenv("AMBIENCE_AUTH_SERVER")
       : "http://localhost:8080";
   // public static final String AUTH_TOKEN = "993d163a59f69f333c28ca5725aedbcc9af547566cb26f258d334bdf824adafa"; // original
   public static final String AUTH_TOKEN = System.getenv("AMBIENCE_AUTH_TOKEN") != null
       ? System.getenv("AMBIENCE_AUTH_TOKEN")
       : "";

   private BuildConfig() {
   }
}
