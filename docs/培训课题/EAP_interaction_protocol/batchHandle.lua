#!/usr/bin/lua
local json = require "luci.json"
local dbg    = require "luci.tools.debug"

dbg.print("arg[1] is "..arg[1])

local function convert_type(val_type)
	if val_type == "Integer" or  val_type == "integer" then
		return "int"
	elseif val_type == "Long" or val_type == "long" then
		return "long"
	elseif val_type == "String" then
		return "string"
	elseif val_type == "Boolean" or val_type == "boolean" then
		return "bool"
	else
		return val_type
	end
	
	

end

local function handle(in_file, out_file)
	local in_file = io.open(in_file)
	os.execute("rm -f %s" % {out_file})
	local out_file = io.open(out_file, "a")
	local key
	local val_type
	local is_key = true
	local key_val_comment
	
	for line in in_file:lines() do
		--dbg.print("line is "..line)
		if is_key then
			key = string.match(line, '@JsonProperty%("(%w+)"%)')
			is_key = false
		else
			val_type = string.match(line, 'private%s*(%w+)%s*%w+;')
			is_key = true
			local key_val = string.format('"%s":,', key)
			key_val_comment = string.format('%-24s//TODO:,type:%s\n', key_val, convert_type(val_type))
			out_file:write(key_val_comment)
		end
	end
	in_file:close()
	out_file:close()
end

local in_file = arg[1]
local out_file = in_file..".out"
handle(in_file, out_file)