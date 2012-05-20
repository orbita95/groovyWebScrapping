@Grab(group='commons-httpclient', module='commons-httpclient', version='3.1')
import org.apache.commons.httpclient.*
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.cookie.CookiePolicy

@Grab(group='org.jsoup', module='jsoup', version='1.6.2')
import org.jsoup.Jsoup

import groovy.json.*


def url = "http://www.bandzone.cz/dirtyblondes"

def httpClient = new HttpClient()
def getUrl = new GetMethod(url)
getUrl.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES)

httpClient.executeMethod(getUrl)
def html = getUrl.getResponseBodyAsString()

//println "Response from ${url} : \${html}"

/* Set-Cookie header from request is somehow malformed (bad domain of origin value) so we have to handle it 
   by ourselves - in this case we need from cookie only session id which is SID cookie parameter. */
def cookieHeader = getUrl.getResponseHeader("Set-Cookie")

/* Session id is needed because when we call ajax api it checks SID against generated hash keys
   eg. http://bandzone.cz/track/play/327662?hash=2af3b047c67791610341fad72fc694a5be854b82 is link
   to api which response with json where is url of real mp3 file - without matching hash parameter
   with session id, bandzone.cz response with "Need to login" response */
def sessionId = cookieHeader.getElements().find { element -> element.name == "SID" }.value


/* finding data in html - in our case we are looking for urls which leads to reveal real mp3 urls of band songs */
println "Finding links in responed html..." 
def document = Jsoup.parse(html)

def elements = document.select("#playlist .track")
def mp3Urls = elements.collect { element ->
    element.attr("data-source")
}

//println "Real mp3 address hiding urls: \n $mp3Urls"

println "Obtaining real mp3 urls from ajax api..."

def realMp3Urls = mp3Urls.collect { mp3Url ->
    def m = new PostMethod(mp3Url)
    /* ignoring cookies as we need to handle them by hand - in normal case when cookies are ok
       (according to specification) HttpClient can handle all the cookie managment by us and we would't do this */
    m.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES) 
    m.setRequestHeader("X-Requested-With", "XMLHttpRequest") // mark it as ajax request (as it do jQuery.ajax call)
    m.setRequestHeader("Cookie", "SID=$sessionId") // without this - bandzone.cz reject with "Need to login" stuff
    httpClient.executeMethod(m)
    //println "-" * 20
    //println "Request header: "
    //println m.getRequestHeaders()
    //println "Response header:"
    //println m.getResponseHeaders()
    def jsonText = m.getResponseBodyAsString() /* it returns json response */
    
    def json = new JsonSlurper().parseText(jsonText)    
    
    json.url
}

/* uncomment this part if you would like to 
   save it all as html file where we can easily start to play/download mp3 */

/*
List.metaClass.collectWithIndex = { cls ->
    def arr = []
    delegate.eachWithIndex { obj, index ->
        arr << cls(obj, index)
    }
    
    return arr
}

def links = realMp3Urls.collectWithIndex { mp3Url, index -> 
    def oneLinkTemplate = """
            <li><a href="$mp3Url"> song $index </a></li>
        """
    
}

def template = """
<html>
<body>
    <ul>
      ${links.join("")}
    </ul>
</body>
</html>
"""

def f = new File("${System.getProperty('user.home')}\\songs.html")
f.text = template
*/