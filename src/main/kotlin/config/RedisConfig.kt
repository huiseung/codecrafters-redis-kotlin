package config

class RedisConfig(
    val config: MutableMap<String, String> = mutableMapOf<String, String>()
) {
    fun initConfig(args: Array<String>) {
        var idx = 0
        config["port"] = "6379"
        config["role"] = "master"

        while(idx < args.size){
            if(args[idx] == "--dir"){
                idx += 1
                config["dir"] = args[idx]
            }
            if(args[idx] == "--dbfilename"){
                idx += 1
                config["dbfilename"] = args[idx]
            }
            if(args[idx] == "--port"){
                idx += 1
                config["port"] = args[idx]
                config["role"] = "slave"
            }
            idx += 1
        }
    }

    fun get(key: String) = config[key]
}
