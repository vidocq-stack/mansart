/**
 * CDI bootstrap — BuildCompatibleExtension that exposes EntityManagerFactory
 * as a CDI bean.
 *
 * No types exported yet; the CDI extension point is provided via ServiceLoader
 * when implementation classes are written.
 */
module io.vidocq.mansart.persistence.cdi {

    requires transitive io.vidocq.mansart.persistence.core;

    requires jakarta.cdi;

    requires jakarta.inject;
    requires jakarta.persistence;

}
