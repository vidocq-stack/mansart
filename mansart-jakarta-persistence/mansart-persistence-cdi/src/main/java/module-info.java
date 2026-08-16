/**
 * CDI 4.1 bootstrap for Mansart Persistence — BuildCompatibleExtension that exposes
 * JPA EntityManagerFactory and EntityManager as beans.
 * Mirror of mansart-data-cdi for Jakarta Persistence.
 */
module io.vidocq.mansart.persistence.cdi {
    requires transitive io.vidocq.mansart.persistence.core;
    requires transitive io.vidocq.mansart.persistence.api;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires jakarta.persistence;

    exports io.vidocq.mansart.persistence.cdi;

    // Vauban (or any compliant CDI Lite container) instantiates beans of this jar via
    // MethodHandles.privateLookupIn. In module-path mode this requires opens-to. We open
    // narrowly to vauban-core only — the package stays sealed for everyone else.
    opens io.vidocq.mansart.persistence.cdi to io.vidocq.vauban.core;

    provides jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension
            with io.vidocq.mansart.persistence.cdi.MansartPersistenceExtension;
}
