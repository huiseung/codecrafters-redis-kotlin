class RedisConfig(
    val config: MutableMap<String, String> = mutableMapOf<String, String>()
) {
    fun initConfig(args: Array<String>) {
        var idx = 0
        while(idx < args.size){
            if(args[idx] == "--dir"){
                idx += 1
                config["dir"] = args[idx]
            }
            if(args[idx] == "--dbfilename"){
                idx += 1
                config["dbfilename"] = args[idx]
            }
            idx += 1
        }
    }

    fun get(key: String) = config[key]
}
