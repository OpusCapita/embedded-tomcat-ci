package man.sab

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

class Options extends OptionsBase {
    @Option(
        name = "configuration",
        help = "Yaml configuration file path",
        defaultValue = ''
    )
    public String configuration;
}
