if(redis.call('get', KEYS[1]) == ARGV[1]) then
   --释放锁 del key
   return redis.call('del',key)
end
return 0
