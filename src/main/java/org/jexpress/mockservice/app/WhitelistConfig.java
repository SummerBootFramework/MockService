package org.jexpress.mockservice.app;

import org.summerboot.jexpress.boot.config.BootConfig;
import org.summerboot.jexpress.boot.config.ConfigUtil;
import org.summerboot.jexpress.boot.config.annotation.Config;
import org.summerboot.jexpress.boot.config.annotation.ConfigHeader;
import org.summerboot.jexpress.boot.config.annotation.ImportResource;

import java.io.File;
import java.util.Properties;
import java.util.Set;

@ImportResource("cfg_whitelist.properties")
public class WhitelistConfig extends BootConfig {

    public static void main(String[] args) {
        String t = generateTemplate(WhitelistConfig.class);
        System.out.println(t);
    }

    public static final WhitelistConfig cfg = new WhitelistConfig();

    private WhitelistConfig() {
    }

    @Override
    protected void loadCustomizedConfigs(File cfgFile, boolean isReal, ConfigUtil helper, Properties props) throws Exception {
    }

    @Override
    public void shutdown() {
    }

    @ConfigHeader(title = "Filter")
    @Config(key = "whitelist", desc = "accept all if not specified, or specify a CSV of URIs or regex of RUL to whitelist, e.g. /myservice/.* , /service1/action1/ , /service1/action2")
    protected volatile Set<String> whteList;

    public Set<String> getWhteList() {
        return whteList;
    }
}
