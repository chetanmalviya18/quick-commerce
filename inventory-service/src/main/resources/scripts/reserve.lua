-- KEYS: one stock per product. ARGV: the quantity wanted foreach key, in the same order.
-- Returns 1 when every product had enough and all were subtracted, otherwise 0 and nothing changes.
for i = 1, #KEYS do
    local available = tonumber(redis.call('GET', KEYS[i]) or '-1')
    if available < tonumber(ARGV[i]) then
        return 0
    end
end
for i = 1, #KEYS do
  redis.call('DECRBY', KEYS[i], ARGV[i])
end
return 1