package com.villamil.barberbooking.application.dto.command;

public record UpdateMarketplaceProfileCommand(String city, String sector, String address, String description,
        String contactPhone, String coverImageUrl) { }
