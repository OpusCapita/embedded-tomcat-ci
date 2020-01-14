package man.sab

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

class Options extends OptionsBase {
    @Option(
        name = "configuration-file-path",
        help = "Configuration file path",
        defaultValue = ''
    )
    public String configurationFilePath;
}
