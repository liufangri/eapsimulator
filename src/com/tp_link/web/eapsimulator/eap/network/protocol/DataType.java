/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-25
 */

package com.tp_link.web.eapsimulator.eap.network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataType {
    public static final int DISCOVERY = 0x0001;
    public static final int PRE_ADOPT_REQUEST = 0x0002;
    public static final int PRE_CONNECT_INFO = 0x0003;
    public static final int ADOPT_REQUEST = 0x0010;
    public static final int ADOPT_RESPONSE = 0x0020;
    public static final int INFORM_REQUEST = 0x0100;
    public static final int INFORM_RESPONSE = 0x0200;
    public static final int SET_REQUEST = 0x1000;
    public static final int SET_RESPONSE = 0x2000;
    public static final int FORGET_REQUEST = 0x4000;
    public static final int FORGET_REQUEST_NO_RESET = 0x20000;
    public static final int UPGRADE_REQUEST = 0x8000;
    public static final int UPGRADE_RESPONSE = 0x10000;

    public static final Map<Integer, String> TYPE_MAP;

    static {
        TYPE_MAP = new LinkedHashMap<>();
        TYPE_MAP.put(DataType.DISCOVERY, "DISCOVERY");
        TYPE_MAP.put(DataType.PRE_ADOPT_REQUEST, "PRE_ADOPT_REQUEST");
        TYPE_MAP.put(DataType.PRE_CONNECT_INFO, "PRE_CONNECT_INFO");
        TYPE_MAP.put(DataType.ADOPT_REQUEST, "ADOPT_REQUEST");
        TYPE_MAP.put(DataType.ADOPT_RESPONSE, "ADOPT_RESPONSE");
        TYPE_MAP.put(DataType.INFORM_REQUEST, "INFORM_REQUEST");
        TYPE_MAP.put(DataType.INFORM_RESPONSE, "INFORM_RESPONSE");
        TYPE_MAP.put(DataType.SET_REQUEST, "SET_REQUEST");
        TYPE_MAP.put(DataType.SET_RESPONSE, "SET_RESPONSE");
        TYPE_MAP.put(DataType.FORGET_REQUEST, "FORGET_REQUEST");
        TYPE_MAP.put(DataType.FORGET_REQUEST_NO_RESET, "FORGET_REQUEST_NO_RESET");
        TYPE_MAP.put(DataType.UPGRADE_RESPONSE, "UPGRADE_RESPONSE");
    }

    public static final String public_key = "MIGJAoGBAJy11IdwtkMDSV7oQLy/FXmeshgN+t73sjfQItvU7y15Y9s4K+0F5RQLmoBcdRHvG4lbQLccIn2EWbzMtcpjvXvfHjpyE4aTqZzHy0g9i4ypTM7XysCwYnqVete9J4KRQJA1seSrX7sGKQH0keUVX9PCEzivH3WIR9EE0rNxoJ7HAgMBAAE=";
    public static final String private_key = "MIICWwIBAAKBgQCctdSHcLZDA0le6EC8vxV5nrIYDfre97I30CLb1O8teWPbOCvtBeUUC5qAXHUR7xuJW0C3HCJ9hFm8zLXKY7173x46chOGk6mcx8tIPYuMqUzO18rAsGJ6lXrXvSeCkUCQNbHkq1+7BikB9JHlFV/TwhM4rx91iEfRBNKzcaCexwIDAQABAoGACTOD7w/nI7glrrTkWDDACgwPWOo5OK5CgJRv94hhIsJl4pFBwwD9mle0EqVbGlp3u3DoM9grDkOfIT7DzcZdcbjsbupfF6XhQ+vdcaRv7Qnwi77ZJrVwm6tCcHBx1BSPqCzEIyFwH9fv/WsCQIv0d1x4CICLfX56shTg9pTyVoECQQDWjPNgdltuVd5a90OVa17A2VrN2APTZcsd9v5XOWz9cgaL6C4F/eR8OZSi6rhM9dbc15q47wXZ75b6kaO2mtQZAkEAuvxD9HlgCZc3blWDu7azX0w5AJMZkGRlOvegyl6dZtnufdvQ4udXVUGQx0vtkULqNJMadrjQ8nTt9dO5pjRl3wJAT/2JF6PI/uAA4MVjayf20cu3sRsiggRnsCpQNVDxs6R5kFvmHNHGCBKnuf3s7LSTgQ7ZXq2u7swG7DC0avZasQJAYJtdcL4VBF+AYB0GhsGKQz5aZRWbK6LzOlgeVvAzWqRWN+iOL+1ejcnmR1HPWDG+V5N5JMWwDtSi7VOKo3iG+QJAKMbKREB6rUx0TestqtfIQ+9KEkQKibgSEX9AkZrhSv7i5T5sfwdJBMmVik6lfzyP6r1xLsp6N9+ZBS8MAxFtXQ==";
}
