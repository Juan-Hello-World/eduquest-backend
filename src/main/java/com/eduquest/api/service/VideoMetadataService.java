package com.eduquest.api.service;

public interface VideoMetadataService {

    VideoInfo resolve(String url);

    record VideoInfo(String videoId, String title) {
    }
}
