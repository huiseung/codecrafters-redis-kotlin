package config

class RedisConfig(
    val config: MutableMap<String, String> = mutableMapOf<String, String>()
) {
    fun initConfig(args: Array<String>) {
        var idx = 0
        config["port"] = "6379"
        config["role"] = "master"

        while (idx < args.size) {
            when(args[idx]){
                "--dir" -> {
                    idx += 1
                    config["dir"] = args[idx]
                }
                "--dbfilename" ->{
                    idx += 1
                    config["dbfilename"] = args[idx]
                }
                "--port" -> {
                    idx += 1
                    config["port"] = args[idx]
                }
                "--replicaof" -> {
                    idx += 1
                    val ret = args[idx].split(" ")
                    config["master_host"] = ret[0]
                    config["master_port"] = ret[1]
                    config["role"] = "slave"
                }
            }
            idx += 1
        }
    }

    fun get(key: String) = config[key]
}
