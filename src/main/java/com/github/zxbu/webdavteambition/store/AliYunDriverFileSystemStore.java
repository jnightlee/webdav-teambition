package com.github.zxbu.webdavteambition.store;

import com.github.zxbu.webdavteambition.model.FileType;
import com.github.zxbu.webdavteambition.model.PathInfo;
import com.github.zxbu.webdavteambition.model.result.TFile;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.Transaction;
import net.sf.webdav.exceptions.WebdavException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Set;

public class AliYunDriverFileSystemStore implements IWebdavStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunDriverFileSystemStore.class);

    private static AliYunDriverClientService aliYunDriverClientService;

    public AliYunDriverFileSystemStore(File file) {
    }

    public static void setBean(AliYunDriverClientService aliYunDriverClientService) {
        AliYunDriverFileSystemStore.aliYunDriverClientService = aliYunDriverClientService;
    }




    @Override
    public void destroy() {
        LOGGER.debug("destroy");

    }

    @Override
    public ITransaction begin(Principal principal, HttpServletRequest req, HttpServletResponse resp) {
        LOGGER.debug("begin");

        aliYunDriverClientService.clearCache();
        return new Transaction(principal, req, resp);
    }

    @Override
    public void checkAuthentication(ITransaction transaction) {
        LOGGER.debug("checkAuthentication");
//        if (transaction.getPrincipal() == null) {
//            throw new UnauthenticatedException(WebdavStatus.SC_UNAUTHORIZED);
//        }
    }

    @Override
    public void commit(ITransaction transaction) {
        aliYunDriverClientService.clearCache();
        LOGGER.debug("commit");
    }

    @Override
    public void rollback(ITransaction transaction) {
        LOGGER.debug("rollback");

    }

    @Override
    public void createFolder(ITransaction transaction, String folderUri) {
        LOGGER.info("createFolder {}", folderUri);

        aliYunDriverClientService.createFolder(folderUri);
    }

    @Override
    public void createResource(ITransaction transaction, String resourceUri) {
        LOGGER.info("createResource {}", resourceUri);

    }

    @Override
    public InputStream getResourceContent(ITransaction transaction, String resourceUri) {
        LOGGER.debug("getResourceContent: {}", resourceUri);
        return aliYunDriverClientService.download(resourceUri);
    }

    @Override
    public long setResourceContent(ITransaction transaction, String resourceUri, InputStream content, String contentType, String characterEncoding) {
        LOGGER.info("setResourceContent {}", resourceUri);
        HttpServletRequest request = transaction.getRequest();
        HttpServletResponse response = transaction.getResponse();

        long contentLength = request.getContentLength();
        if (contentLength < 0) {
            contentLength = Long.parseLong(request.getHeader("content-length"));
        }
        aliYunDriverClientService.uploadPre(resourceUri, contentLength, content);

        if (contentLength == 0) {
            String expect = request.getHeader("Expect");

            // 支持大文件上传
            if ("100-continue".equalsIgnoreCase(expect)) {
                try {
                    response.sendError(100, "Continue");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        }
        return contentLength;
    }

    @Override
    public String[] getChildrenNames(ITransaction transaction, String folderUri) {
        LOGGER.debug("getChildrenNames: {}", folderUri);
        TFile tFile = aliYunDriverClientService.getTFileByPath(folderUri);
        if (tFile.getType().equals(FileType.file.name())) {
            return new String[0];
        }
        Set<TFile> tFileList = aliYunDriverClientService.getTFiles(tFile.getFile_id());
        return tFileList.stream().map(TFile::getName).toArray(String[]::new);
    }



    @Override
    public long getResourceLength(ITransaction transaction, String path) {
        LOGGER.debug("getResourceLength: {}", path);
        TFile tFile = aliYunDriverClientService.getTFileByPath(path);
        if (tFile == null || tFile.getSize() == null) {
            return 384;
        }

        return tFile.getSize();
    }

    @Override
    public void removeObject(ITransaction transaction, String uri) {
        LOGGER.info("removeObject: {}", uri);
        aliYunDriverClientService.remove(uri);
    }

    @Override
    public boolean moveObject(ITransaction transaction, String destinationPath, String sourcePath) {
        LOGGER.info("moveObject, destinationPath={}, sourcePath={}", destinationPath, sourcePath);

        PathInfo destinationPathInfo = aliYunDriverClientService.getPathInfo(destinationPath);
        PathInfo sourcePathInfo = aliYunDriverClientService.getPathInfo(sourcePath);
        // 名字相同，说明是移动目录
        if (sourcePathInfo.getName().equals(destinationPathInfo.getName())) {
            aliYunDriverClientService.move(sourcePath, destinationPathInfo.getParentPath());
        } else {
            if (!destinationPathInfo.getParentPath().equals(sourcePathInfo.getParentPath())) {
                throw new WebdavException("不支持目录和名字同时修改");
            }
            // 名字不同，说明是修改名字。不考虑目录和名字同时修改的情况
            aliYunDriverClientService.rename(sourcePath, destinationPathInfo.getName());
        }
        return true;
    }

    @Override
    public StoredObject getStoredObject(ITransaction transaction, String uri) {


        LOGGER.debug("getStoredObject: {}", uri);
        TFile tFile = aliYunDriverClientService.getTFileByPath(uri);
        if (tFile != null) {
            StoredObject so = new StoredObject();
            so.setFolder(tFile.getType().equalsIgnoreCase("folder"));
            so.setResourceLength(getResourceLength(transaction, uri));
            so.setCreationDate(tFile.getCreated_at());
            so.setLastModified(tFile.getUpdated_at());
            return so;
        }

        return null;
    }


}
