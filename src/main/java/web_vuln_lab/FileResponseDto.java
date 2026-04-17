package web_vuln_lab;

public class FileResponseDto {

    private Long id;
    private String originalName;
    private String storedName;
    private String contentType;
    private Long size;
    private Long ownerId;
    private String ownerUsername;

    public FileResponseDto(Long id, String originalName, String storedName,
                           String contentType, Long size,
                           Long ownerId, String ownerUsername) {
        this.id = id;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.size = size;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSize() {
        return size;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }
}