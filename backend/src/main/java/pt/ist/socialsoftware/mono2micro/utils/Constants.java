package pt.ist.socialsoftware.mono2micro.utils;

public final class Constants {
    
  private Constants(){
  }
  
  public static String CODEBASES_PATH = "src/main/resources/codebases/";
  public static String RESOURCES_PATH = "src/main/resources/";
  public static String MOJO_RESOURCES_PATH = "src/main/java/pt/ist/socialsoftware/mono2micro/utils/mojoCalculator/" +
          "src/main/resources/";
  public static String PYTHON = PropertiesManager.getProperties().getProperty("python");
  public static final String DEFAULT_REDESIGN_NAME = "Monolith Trace";

  public enum TraceType {
    ALL,
    REPRESENTATIVE,
    LONGEST,
    WITH_MORE_DIFFERENT_ACCESSES,
  }

  enum Mode {
    READ,
    WRITE,
    READWRITE
  }
}