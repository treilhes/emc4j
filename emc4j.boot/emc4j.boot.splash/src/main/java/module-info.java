import com.treilhes.emc4j.boot.api.loader.BootContextConfigClasses;
import com.treilhes.emc4j.boot.splash.SplashBootClasses;

module emc4j.boot.splash {
	exports com.treilhes.emc4j.boot.splash;
	exports com.treilhes.emc4j.boot.splash.impl;

	requires transitive emc4j.boot.api;

    requires emc4j.boot.starter;

    provides BootContextConfigClasses with SplashBootClasses;
}