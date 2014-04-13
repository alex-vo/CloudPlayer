package controller;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 6/21/13
 * Time: 7:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestControlFilter implements Filter {

    private Set<String> localAddresses = new HashSet<String>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            localAddresses.add(InetAddress.getLocalHost().getHostAddress());
            for (InetAddress inetAddress : InetAddress.getAllByName("localhost")) {
                localAddresses.add(inetAddress.getHostAddress());
            }
        } catch (IOException e) {
            throw new ServletException("Unable to lookup local addresses");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        if( isIncorrectRequest(servletRequest) ){
            ((HttpServletResponse)servletResponse).sendRedirect("welcome");
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private boolean isIncorrectRequest(ServletRequest servletRequest){
        String uri = ((HttpServletRequest) servletRequest).getRequestURI();
        return  (!uri.endsWith("/register") && !uri.endsWith("/registerForm") && !uri.endsWith("/welcome")
                && !uri.endsWith("/login") && !localAddresses.contains(servletRequest.getRemoteAddr())
                && ((HttpServletRequest) servletRequest).getSession().getAttribute("user") == null);
    }

}
