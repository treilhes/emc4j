import com.treilhes.emc4j.boot.api.loader.extension.Extension;

import app.root_merged.EmcExtension;

module root.merged {
    exports app.root_merged;
    exports app.root_merged.controller to spring.beans, spring.web;

    requires emc4j.boot.starter;
    requires emc4j.boot.api;

    provides Extension with EmcExtension;
}