package com.sscl.sdnetmonitor.controller;

import com.sscl.sdnetmonitor.dto.JunctionDto;
import com.sscl.sdnetmonitor.service.JunctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sdnet-monitor/junctions")
@RequiredArgsConstructor
public class JunctionController {

    private final JunctionService junctionService;

    @GetMapping
    public List<JunctionDto> findAll() {
        return junctionService.findAll();
    }

    @GetMapping("/{id}")
    public JunctionDto findById(@PathVariable String id) {
        return junctionService.findById(id);
    }
}
