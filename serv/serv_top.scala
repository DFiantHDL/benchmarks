// DFHDL port of serv_top.v, the SERV bit-serial RISC-V CPU by Olof Kindgren, fixed at the
// RTLMeter benchmark configuration: W = 1, WITH_CSR = 1, PRE_REGISTER = 1,
// RESET_STRATEGY = "MINI", RESET_PC = 0, no MDU / compressed / aligner and no debug module.
// Port-wide conventions (all serv/servile/servant designs): each design carries its baseline
// Verilog module port names case-sensitively (with the i_/o_ direction prefixes) and lives in a
// file named after that module. The clock/reset are DFHDL magnets configured per module via
// `@hw.constraints.timing.clock/reset(portName = ...)` annotations (or, for `serv_state`, an
// explicit `Rst` port it also reads combinationally); modules without a reset carry a clock-only
// annotation. Every register carries a power-up init so all simulators (2-state and 4-state)
// agree bit-for-bit, and the MINI reset values fold into those register inits (DFHDL auto-reset).
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_top.v: the SERV core, wiring the ten sub-modules together. Clocked on `clk` with the
  * synchronous MINI reset `i_rst`.
  */
@hw.constraints.timing.clock(portName = "clk")
@hw.constraints.timing.reset(portName = "i_rst")
class serv_top extends RTDesign:
  val i_timer_irq = Bit <> IN
  // RF interface
  val o_rf_rreq = Bit <> OUT
  val o_rf_wreq = Bit <> OUT
  val i_rf_ready = Bit <> IN
  val o_wreg0 = Bits(6) <> OUT
  val o_wreg1 = Bits(6) <> OUT
  val o_wen0 = Bit <> OUT
  val o_wen1 = Bit <> OUT
  val o_wdata0 = Bit <> OUT
  val o_wdata1 = Bit <> OUT
  val o_rreg0 = Bits(6) <> OUT
  val o_rreg1 = Bits(6) <> OUT
  val i_rdata0 = Bit <> IN
  val i_rdata1 = Bit <> IN
  // instruction bus
  val o_ibus_adr = Bits(32) <> OUT
  val o_ibus_cyc = Bit <> OUT
  val i_ibus_rdt = Bits(32) <> IN
  val i_ibus_ack = Bit <> IN
  // data bus
  val o_dbus_adr = Bits(32) <> OUT
  val o_dbus_dat = Bits(32) <> OUT
  val o_dbus_sel = Bits(4) <> OUT
  val o_dbus_we = Bit <> OUT
  val o_dbus_cyc = Bit <> OUT
  val i_dbus_rdt = Bits(32) <> IN
  val i_dbus_ack = Bit <> IN

  val state = serv_state()
  val decode = serv_decode()
  val immdec = serv_immdec()
  val bufreg = serv_bufreg()
  val bufreg2 = serv_bufreg2()
  val ctrl = serv_ctrl()
  val alu = serv_alu()
  val rf_if = serv_rf_if()
  val mem_if = serv_mem_if()
  val csr = serv_csr()

  state.i_new_irq <> csr.o_new_irq
  state.i_alu_cmp <> alu.o_cmp
  state.i_ctrl_misalign <> bufreg.o_lsb(1)
  state.i_sh_done <> bufreg2.o_sh_done
  state.i_mem_misalign <> mem_if.o_misalign
  state.i_bne_or_bge <> decode.o_bne_or_bge
  state.i_cond_branch <> decode.o_cond_branch
  state.i_dbus_en <> decode.o_dbus_en
  state.i_two_stage_op <> decode.o_two_stage_op
  state.i_branch_op <> decode.o_branch_op
  state.i_shift_op <> decode.o_shift_op
  state.i_sh_right <> decode.o_sh_right
  state.i_alu_rd_sel1 <> decode.o_alu_rd_sel(1)
  state.i_rd_alu_en <> decode.o_rd_alu_en
  state.i_e_op <> decode.o_e_op
  state.i_rd_op <> decode.o_rd_op
  state.i_dbus_ack <> i_dbus_ack
  state.i_ibus_ack <> i_ibus_ack
  state.i_rf_ready <> i_rf_ready
  o_dbus_cyc <> state.o_dbus_cyc
  o_ibus_cyc <> state.o_ibus_cyc
  o_rf_rreq <> state.o_rf_rreq
  o_rf_wreq <> state.o_rf_wreq

  decode.i_wb_rdt <> i_ibus_rdt
  decode.i_wb_en <> i_ibus_ack

  immdec.i_cnt_en <> state.o_cnt_en
  immdec.i_cnt_done <> state.o_cnt_done
  immdec.i_immdec_en <> decode.o_immdec_en
  immdec.i_csr_imm_en <> decode.o_csr_imm_en
  immdec.i_ctrl <> decode.o_immdec_ctrl
  immdec.i_wb_en <> i_ibus_ack
  immdec.i_wb_rdt <> i_ibus_rdt

  bufreg.i_cnt0 <> state.o_cnt0
  bufreg.i_cnt1 <> state.o_cnt1
  bufreg.i_en <> state.o_bufreg_en
  bufreg.i_init <> state.o_init
  bufreg.i_rs1_en <> decode.o_bufreg_rs1_en
  bufreg.i_imm_en <> decode.o_bufreg_imm_en
  bufreg.i_clr_lsb <> decode.o_bufreg_clr_lsb
  bufreg.i_sh_signed <> decode.o_bufreg_sh_signed
  bufreg.i_rs1 <> rf_if.o_rs1
  bufreg.i_imm <> immdec.o_imm
  o_dbus_adr <> bufreg.o_dbus_adr

  bufreg2.i_en <> state.o_cnt_en
  bufreg2.i_init <> state.o_init
  bufreg2.i_cnt7 <> state.o_cnt7
  bufreg2.i_cnt_done <> state.o_cnt_done
  bufreg2.i_sh_right <> decode.o_sh_right
  bufreg2.i_lsb <> bufreg.o_lsb
  bufreg2.i_bytecnt <> state.o_mem_bytecnt
  bufreg2.i_op_b_sel <> decode.o_op_b_source
  bufreg2.i_shift_op <> decode.o_shift_op
  bufreg2.i_rs2 <> rf_if.o_rs2
  bufreg2.i_imm <> immdec.o_imm
  bufreg2.i_load <> i_dbus_ack
  bufreg2.i_dat <> i_dbus_rdt
  o_dbus_dat <> bufreg2.o_dat

  ctrl.i_pc_en <> state.o_ctrl_pc_en
  ctrl.i_cnt12to31 <> state.o_cnt12to31
  ctrl.i_cnt0 <> state.o_cnt0
  ctrl.i_cnt1 <> state.o_cnt1
  ctrl.i_cnt2 <> state.o_cnt2
  ctrl.i_jump <> state.o_ctrl_jump
  ctrl.i_jal_or_jalr <> decode.o_ctrl_jal_or_jalr
  ctrl.i_utype <> decode.o_ctrl_utype
  ctrl.i_pc_rel <> decode.o_ctrl_pc_rel
  ctrl.i_trap <> (state.o_ctrl_trap || decode.o_ctrl_mret)
  ctrl.i_imm <> immdec.o_imm
  ctrl.i_buf <> bufreg.o_q
  ctrl.i_csr_pc <> rf_if.o_csr_pc
  o_ibus_adr <> ctrl.o_ibus_adr

  alu.i_en <> state.o_cnt_en
  alu.i_cnt0 <> state.o_cnt0
  alu.i_sub <> decode.o_alu_sub
  alu.i_bool_op <> decode.o_alu_bool_op
  alu.i_cmp_eq <> decode.o_alu_cmp_eq
  alu.i_cmp_sig <> decode.o_alu_cmp_sig
  alu.i_rd_sel <> decode.o_alu_rd_sel
  alu.i_rs1 <> rf_if.o_rs1
  alu.i_op_b <> bufreg2.o_op_b
  alu.i_buf <> bufreg.o_q

  rf_if.i_cnt_en <> state.o_cnt_en
  rf_if.i_rdata0 <> i_rdata0
  rf_if.i_rdata1 <> i_rdata1
  rf_if.i_trap <> state.o_ctrl_trap
  rf_if.i_mret <> decode.o_ctrl_mret
  rf_if.i_mepc <> ctrl.o_ibus_adr(0)
  rf_if.i_mtval_pc <> decode.o_mtval_pc
  rf_if.i_bufreg_q <> bufreg.o_q
  rf_if.i_bad_pc <> ctrl.o_bad_pc
  rf_if.i_csr_en <> decode.o_csr_en
  rf_if.i_csr_addr <> decode.o_csr_addr
  rf_if.i_csr <> csr.o_csr_in
  rf_if.i_rd_wen <> state.o_rf_rd_en
  rf_if.i_rd_waddr <> immdec.o_rd_addr
  rf_if.i_ctrl_rd <> ctrl.o_rd
  rf_if.i_alu_rd <> alu.o_rd
  rf_if.i_rd_alu_en <> decode.o_rd_alu_en
  rf_if.i_csr_rd <> csr.o_q
  rf_if.i_rd_csr_en <> decode.o_rd_csr_en
  rf_if.i_mem_rd <> mem_if.o_rd
  rf_if.i_rd_mem_en <> decode.o_rd_mem_en
  rf_if.i_rs1_raddr <> immdec.o_rs1_addr
  rf_if.i_rs2_raddr <> immdec.o_rs2_addr
  o_wreg0 <> rf_if.o_wreg0
  o_wreg1 <> rf_if.o_wreg1
  o_wen0 <> rf_if.o_wen0
  o_wen1 <> rf_if.o_wen1
  o_wdata0 <> rf_if.o_wdata0
  o_wdata1 <> rf_if.o_wdata1
  o_rreg0 <> rf_if.o_rreg0
  o_rreg1 <> rf_if.o_rreg1

  mem_if.i_bytecnt <> state.o_mem_bytecnt
  mem_if.i_lsb <> bufreg.o_lsb
  mem_if.i_signed <> decode.o_mem_signed
  mem_if.i_word <> decode.o_mem_word
  mem_if.i_half <> decode.o_mem_half
  mem_if.i_bufreg2_q <> bufreg2.o_q
  o_dbus_sel <> mem_if.o_wb_sel
  o_dbus_we <> decode.o_mem_cmd

  csr.i_trig_irq <> i_ibus_ack
  csr.i_en <> state.o_cnt_en
  csr.i_cnt0to3 <> state.o_cnt0to3
  csr.i_cnt3 <> state.o_cnt3
  csr.i_cnt7 <> state.o_cnt7
  csr.i_cnt11 <> state.o_cnt11
  csr.i_cnt12 <> state.o_cnt12
  csr.i_cnt_done <> state.o_cnt_done
  csr.i_mem_op <> !decode.o_mtval_pc
  csr.i_mtip <> i_timer_irq
  csr.i_trap <> state.o_ctrl_trap
  csr.i_e_op <> decode.o_e_op
  csr.i_ebreak <> decode.o_ebreak
  csr.i_mem_cmd <> decode.o_mem_cmd
  csr.i_mstatus_en <> decode.o_csr_mstatus_en
  csr.i_mie_en <> decode.o_csr_mie_en
  csr.i_mcause_en <> decode.o_csr_mcause_en
  csr.i_csr_source <> decode.o_csr_source
  csr.i_mret <> decode.o_ctrl_mret
  csr.i_csr_d_sel <> decode.o_csr_d_sel
  csr.i_rf_csr_out <> rf_if.o_csr
  csr.i_csr_imm <> immdec.o_csr_imm
  csr.i_rs1 <> rf_if.o_rs1
end serv_top
