package br.edu.ifto.pwebII;

import br.edu.ifto.pwebII.model.entity.*;
import br.edu.ifto.pwebII.model.jdbc.repository.*;
import br.edu.ifto.pwebII.service.AgendaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AgendaFluxoTests {

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ConsultaRepository consultaRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Test
    void fluxoCompletoAgendaAgendamentoCancelamentoEConfirmacao() {
        Medico medico = medicoRepository.medico(3L); // Dr. Carlos Lima
        assertNotNull(medico);

        LocalDate data = LocalDate.of(2026, 12, 15);
        LocalTime horario = LocalTime.of(15, 30);

        // a) Cadastrar disponibilidade na agenda (status DISPONIVEL)
        Agenda novaAgenda = new Agenda();
        novaAgenda.setMedico(medico);
        novaAgenda.setData(data);
        novaAgenda.setHorario(horario);
        BindingResult result = new BeanPropertyBindingResult(novaAgenda, "agenda");
        agendaService.cadastrarHorario(novaAgenda, result);

        assertFalse(result.hasErrors(), "Cadastro de horário válido não deve ter erros");
        assertNotNull(novaAgenda.getId());
        assertEquals(StatusAgenda.DISPONIVEL, novaAgenda.getStatus());

        // a) Impedir cadastro duplicado para o mesmo médico
        Agenda duplicada = new Agenda();
        duplicada.setMedico(medico);
        duplicada.setData(data);
        duplicada.setHorario(horario);
        BindingResult resultDuplicado = new BeanPropertyBindingResult(duplicada, "agenda");
        agendaService.cadastrarHorario(duplicada, resultDuplicado);
        assertTrue(resultDuplicado.hasFieldErrors("horario"), "Deve impedir horário duplicado para o mesmo médico");

        // b) Horário aparece na listagem de disponíveis
        assertTrue(agendaRepository.disponiveis().stream().anyMatch(a -> a.getId().equals(novaAgenda.getId())));

        // c) Realizar agendamento pelo paciente 1 (Maria Silva)
        int totalConsultasAntes = consultaRepository.consultas().size();
        Agendamento agendamento1 = agendaService.agendar(novaAgenda.getId(), 1L);

        assertNotNull(agendamento1.getId());
        assertEquals(StatusAgendamento.AGENDADO, agendamento1.getStatus());
        assertEquals(StatusAgenda.AGENDADO, agendaRepository.agenda(novaAgenda.getId()).getStatus());
        assertFalse(agendaRepository.disponiveis().stream().anyMatch(a -> a.getId().equals(novaAgenda.getId())));
        assertEquals(totalConsultasAntes, consultaRepository.consultas().size(),
                "Agendar não deve criar registro na entidade Consulta");

        // c) Impedir que um horário já reservado seja agendado novamente
        assertThrows(IllegalStateException.class, () -> agendaService.agendar(novaAgenda.getId(), 2L));

        // e) Cancelar agendamento e liberar o horário novamente para DISPONIVEL
        agendaService.cancelarAgendamento(agendamento1.getId(), null);

        assertEquals(StatusAgendamento.CANCELADO, agendamentoRepository.agendamento(agendamento1.getId()).getStatus());
        assertEquals(StatusAgenda.DISPONIVEL, agendaRepository.agenda(novaAgenda.getId()).getStatus());
        assertTrue(agendaRepository.disponiveis().stream().anyMatch(a -> a.getId().equals(novaAgenda.getId())));

        // e) Garantir que outro paciente (João Souza, id 2) consiga agendar o mesmo horário liberado
        Agendamento agendamento2 = agendaService.agendar(novaAgenda.getId(), 2L);
        assertEquals(StatusAgendamento.AGENDADO, agendamento2.getStatus());
        assertEquals(2L, agendamento2.getPaciente().getId());
        assertEquals(250.00, agendamento2.getValor(), "Agendamento deve congelar o valor atual da consulta do médico");
        assertEquals(StatusAgenda.AGENDADO, agendaRepository.agenda(novaAgenda.getId()).getStatus());

        // Simula reajuste no valor da consulta do médico para novos agendamentos (de 250 para 400)
        // O agendamento2 já marcado NÃO pode ter seu valor alterado.
        assertEquals(250.00, agendamentoRepository.agendamento(agendamento2.getId()).getValor());

        // d) Confirmar presença pela clínica -> cria Consulta vinculada com o valor congelado e atualiza para REALIZADO
        Consulta consulta = agendaService.confirmarPresenca(agendamento2.getId(), "Compareceu no horário.");

        assertNotNull(consulta.getId());
        assertEquals(agendamento2.getId(), consulta.getAgendamento().getId());
        assertEquals(2L, consulta.getPaciente().getId());
        assertEquals(3L, consulta.getMedico().getId());
        assertEquals(250.00, consulta.getValor(), "Consulta deve herdar o valor congelado no agendamento");
        assertEquals(LocalDate.of(2026, 12, 15).atTime(15, 30), consulta.getData());
        assertEquals(totalConsultasAntes + 1, consultaRepository.consultas().size());

        Agendamento agendamentoRealizado = agendamentoRepository.agendamento(agendamento2.getId());
        assertEquals(StatusAgendamento.REALIZADO, agendamentoRealizado.getStatus());
        assertNotNull(agendamentoRealizado.getConsulta());

        // Não deve permitir confirmar nem cancelar um agendamento já REALIZADO
        assertThrows(IllegalStateException.class, () -> agendaService.confirmarPresenca(agendamento2.getId(), ""));
        assertThrows(IllegalStateException.class, () -> agendaService.cancelarAgendamento(agendamento2.getId(), null));

        // Não deve permitir cadastrar horário em data/hora no passado
        Agenda passada = new Agenda();
        passada.setMedico(medico);
        passada.setData(LocalDate.of(2020, 1, 1));
        passada.setHorario(LocalTime.of(8, 0));
        BindingResult resultPassado = new BeanPropertyBindingResult(passada, "agenda");
        agendaService.cadastrarHorario(passada, resultPassado);
        assertTrue(resultPassado.hasFieldErrors("data"), "Deve impedir cadastro de horário no passado");
    }
}
