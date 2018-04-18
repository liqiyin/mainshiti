### 链接

<http://blog.csdn.net/yusiguyuan/article/details/38920295>  
<http://igoro.com/archive/what-really-happens-when-you-navigate-to-a-url/>

---

### 在浏览器里输入网址

### 浏览器查找域名的IP地址

* 浏览器缓存 – 浏览器会缓存DNS记录一段时间。 有趣的是，操作系统没有告诉浏览器储存DNS记录的时间，这样不同浏览器会储存个自固定的一个时间（2分钟到30分钟不等）。
* 系统缓存 – 如果在浏览器缓存里没有找到需要的记录，浏览器会做一个系统调用（windows里是gethostbyname）。这样便可获得系统缓存中的记录。
* 路由器缓存 – 接着，前面的查询请求发向路由器，它一般会有自己的DNS缓存。
* ISP DNS 缓存 – 接下来要check的就是ISP缓存DNS的服务器。在这一般都能找到相应的缓存记录。
* 递归搜索 – 你的ISP的DNS服务器从跟域名服务器开始进行递归搜索，从.com顶级域名服务器到Facebook的域名服务器。一般DNS服务器的缓存中会有.com域名服务器中的域名，所以到顶级服务器的匹配过程不是那么必要了。

### 浏览器给web服务器发送一个HTTP请求

GET 这个请求定义了要读取的URL： “http://facebook.com/”。 浏览器自身定义 (User-Agent 头)， 和它希望接受什么类型的相应 (Accept andAccept-Encoding 头). Connection头要求服务器为了后边的请求不要关闭TCP连接。

请求中也包含浏览器存储的该域名的cookies。可能你已经知道，在不同页面请求当中，cookies是与跟踪一个网站状态相匹配的键值。这样cookies会存储登录用户名，服务器分配的密码和一些用户设置等。Cookies会以文本文档形式存储在客户机里，每次请求时发送给服务器。

### facebook服务的永久重定向响应

服务器给浏览器响应一个301永久重定向响应，这样浏览器就会访问“http://www.facebook.com/” 而非“http://facebook.com/”。

### 浏览器跟踪重定向地址

现在，浏览器知道了“http://www.facebook.com/”才是要访问的正确地址，所以它会发送另一个获取请求

### 服务器“处理”请求

服务器接收到获取请求，然后处理并返回一个响应。

#### Web 服务器软件
web服务器软件（像IIS和阿帕奇）接收到HTTP请求，然后确定执行什么请求处理来处理它。请求处理就是一个能够读懂请求并且能生成HTML来进行响应的程序（像ASP.NET,PHP,RUBY...）。
举 个最简单的例子，需求处理可以以映射网站地址结构的文件层次存储。像http://example.com/folder1/page1.aspx这个地 址会映射/httpdocs/folder1/page1.aspx这个文件。web服务器软件可以设置成为地址人工的对应请求处理，这样 page1.aspx的发布地址就可以是http://example.com/folder1/page1。

#### 请求处理
请求处理阅读请求及它的参数和cookies。它会读取也可能更新一些数据，并讲数据存储在服务器上。然后，需求处理会生成一个HTML响应。

### 服务器发回一个HTML响应

### 浏览器开始显示HTML
