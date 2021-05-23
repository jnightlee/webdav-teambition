package com.github.zxbu.webdavteambition.filter;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

public class ErrorFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        ErrorWrapperResponse wrapperResponse = new ErrorWrapperResponse(httpServletResponse);

        try {
            filterChain.doFilter(httpServletRequest, wrapperResponse);
            if (wrapperResponse.hasErrorToSend()) {
                httpServletResponse.setStatus(wrapperResponse.getStatus());
                if (wrapperResponse.getMessage() != null) {
                    httpServletResponse.getWriter().write(wrapperResponse.getMessage());
                }
            }
            httpServletResponse.flushBuffer();
        } catch (Throwable t) {
            httpServletResponse.setStatus(500);
            httpServletResponse.getWriter().write(t.getMessage());
            httpServletResponse.flushBuffer();
        }
    }

    private static class ErrorWrapperResponse extends HttpServletResponseWrapper {
        private int status;
        private String message;
        private boolean hasErrorToSend = false;

        ErrorWrapperResponse(HttpServletResponse response) {
            super(response);
        }

        public void sendError(int status) throws IOException {
            this.sendError(status, (String) null);
        }

        public void sendError(int status, String message) throws IOException {
            this.status = status;
            this.message = message;
            this.hasErrorToSend = true;
        }

        public int getStatus() {
            return this.hasErrorToSend ? this.status : super.getStatus();
        }

        public void flushBuffer() throws IOException {
            super.flushBuffer();
        }


        String getMessage() {
            return this.message;
        }

        boolean hasErrorToSend() {
            return this.hasErrorToSend;
        }

        public PrintWriter getWriter() throws IOException {
            return super.getWriter();
        }

        public ServletOutputStream getOutputStream() throws IOException {
            return super.getOutputStream();
        }
    }
}
