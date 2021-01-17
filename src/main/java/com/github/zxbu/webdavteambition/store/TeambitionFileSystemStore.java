package com.github.zxbu.webdavteambition.store;

import com.github.zxbu.webdavteambition.model.PathInfo;
import com.github.zxbu.webdavteambition.model.result.TFile;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.security.Principal;
import java.util.Date;
import java.util.List;

public class TeambitionFileSystemStore implements IWebdavStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeambitionFileSystemStore.class);

    private static TeambitionClientService teambitionClientService;

    public TeambitionFileSystemStore(File file) {
    }

    public static void setBean(TeambitionClientService teambitionClientService) {
        TeambitionFileSystemStore.teambitionClientService = teambitionClientService;
    }




    @Override
    public void destroy() {
        LOGGER.debug("destroy");

    }

    @Override
    public ITransaction begin(Principal principal) {
        LOGGER.debug("begin");
        return null;
    }

    @Override
    public void checkAuthentication(ITransaction transaction) {
        LOGGER.debug("checkAuthentication");

    }

    @Override
    public void commit(ITransaction transaction) {
        LOGGER.debug("commit");
    }

    @Override
    public void rollback(ITransaction transaction) {
        LOGGER.debug("rollback");

    }

    @Override
    public void createFolder(ITransaction transaction, String folderUri) {
        LOGGER.info("createFolder {}", folderUri);

        teambitionClientService.createFolder(folderUri);
    }

    @Override
    public void createResource(ITransaction transaction, String resourceUri) {
        LOGGER.info("createResource {}", resourceUri);

    }

    @Override
    public InputStream getResourceContent(ITransaction transaction, String resourceUri) {
        LOGGER.debug("getResourceContent: {}", resourceUri);
        return teambitionClientService.download(resourceUri);
    }

    @Override
    public long setResourceContent(ITransaction transaction, String resourceUri, InputStream content, String contentType, String characterEncoding) {
        LOGGER.info("setResourceContent {}", resourceUri);
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        int contentLength = request.getContentLength();
        if (contentLength < 0) {
            contentLength = 0;
        }
        teambitionClientService.uploadPre(resourceUri, contentLength, content);
        return contentLength;
    }

    @Override
    public String[] getChildrenNames(ITransaction transaction, String folderUri) {
        LOGGER.debug("getChildrenNames: {}", folderUri);
        TFile tFile = teambitionClientService.getTFileByPath(folderUri);
        List<TFile> tFileList = teambitionClientService.getTFiles(tFile.getNodeId());
        return tFileList.stream().map(TFile::getName).toArray(String[]::new);
    }



    @Override
    public long getResourceLength(ITransaction transaction, String path) {
        LOGGER.debug("getResourceLength: {}", path);
        TFile tFile = teambitionClientService.getTFileByPath(path);
        if (tFile == null || tFile.getSize() == null) {
            return 384;
        }

        return tFile.getSize();
    }

    @Override
    public void removeObject(ITransaction transaction, String uri) {
        LOGGER.info("removeObject: {}", uri);
        teambitionClientService.remove(uri);
    }

    @Override
    public boolean moveObject(ITransaction transaction, String destinationPath, String sourcePath) {
        LOGGER.info("moveObject, destinationPath={}, sourcePath={}", destinationPath, sourcePath);

        PathInfo destinationPathInfo = teambitionClientService.getPathInfo(destinationPath);
        PathInfo sourcePathInfo = teambitionClientService.getPathInfo(sourcePath);
        // 名字相同，说明是移动目录
        if (sourcePathInfo.getName().equals(destinationPathInfo.getName())) {
            teambitionClientService.move(sourcePath, destinationPathInfo.getParentPath());
        } else {
            if (!destinationPathInfo.getParentPath().equals(sourcePathInfo.getParentPath())) {
                throw new RuntimeException("不支持目录和名字同时修改");
            }
            // 名字不同，说明是修改名字。不考虑目录和名字同时修改的情况
            teambitionClientService.rename(sourcePath, destinationPathInfo.getName());
        }
        return true;
    }

    @Override
    public StoredObject getStoredObject(ITransaction transaction, String uri) {
        LOGGER.debug("getStoredObject: {}", uri);
        TFile tFile = teambitionClientService.getTFileByPath(uri);
        if (tFile != null) {
            StoredObject so = new StoredObject();
            so.setFolder(tFile.getKind().equalsIgnoreCase("folder"));
            so.setResourceLength(getResourceLength(transaction, uri));
            so.setCreationDate(tFile.getCreated());
            so.setLastModified(tFile.getUpdated());
            return so;
        }

        return null;
    }


}
