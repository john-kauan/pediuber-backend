package com.pediuber.pediuber.logging;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {

    private String timestamp;

    private String evento;

    private Long corridaId;

    private String servicoOrigem;

    private String estadoAnterior;

    private String estadoNovo;

    private String level;

}
