module mansart.jakarta.persistence {
    requires jakarta.persistence;
    requires java.class.file;
    requires java.base;
    
    exports ee.jakarta.tck.persistence.spi;
}