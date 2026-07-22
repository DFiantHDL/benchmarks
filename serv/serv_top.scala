// DFHDL port of serv_top.v, the SERV bit-serial RISC-V CPU by Olof Kindgren, fixed at the
// RTLMeter benchmark configuration: W = 1, WITH_CSR = 1, PRE_REGISTER = 1,
// RESET_STRATEGY = "MINI", RESET_PC = 0, no MDU / compressed / aligner and no debug module.
// Port-wide conventions (all serv/servile/servant designs): the synchronous MINI reset is
// explicit logic on a plain `wb_rst` input port because the original also uses the reset
// combinationally; every register carries a power-up init so all simulators (2-state and
// 4-state) agree bit-for-bit; in generated HDL those inits load on the implicit `rst` port
// (external harnesses assert it for one preamble cycle), while DFacsimile applies them at
// time zero. Each design lives in a file named after its baseline Verilog module.
// Origin: https://github.com/olofk/serv @ 7d9cde4e6ca4f4c84d7512a752a4c187537dd373
// SPDX-FileCopyrightText: 2018 Olof Kindgren <olof.kindgren@gmail.com>
// SPDX-License-Identifier: ISC
package dfhdl.benchmarks.serv

import dfhdl.*

/** serv_top.v: the SERV core, wiring the ten sub-modules together. */
class serv_top extends RTDesign:
  val wb_rst = Bit <> IN
  val timer_irq = Bit <> IN
  // RF interface
  val rf_rreq = Bit <> OUT
  val rf_wreq = Bit <> OUT
  val rf_ready = Bit <> IN
  val wreg0 = Bits(6) <> OUT
  val wreg1 = Bits(6) <> OUT
  val wen0 = Bit <> OUT
  val wen1 = Bit <> OUT
  val wdata0 = Bit <> OUT
  val wdata1 = Bit <> OUT
  val rreg0 = Bits(6) <> OUT
  val rreg1 = Bits(6) <> OUT
  val rdata0 = Bit <> IN
  val rdata1 = Bit <> IN
  // instruction bus
  val ibus_adr = Bits(32) <> OUT
  val ibus_cyc = Bit <> OUT
  val ibus_rdt = Bits(32) <> IN
  val ibus_ack = Bit <> IN
  // data bus
  val dbus_adr = Bits(32) <> OUT
  val dbus_dat = Bits(32) <> OUT
  val dbus_sel = Bits(4) <> OUT
  val dbus_we = Bit <> OUT
  val dbus_cyc = Bit <> OUT
  val dbus_rdt = Bits(32) <> IN
  val dbus_ack = Bit <> IN

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
  state.wb_rst <> wb_rst
  state.new_irq <> csr.new_irq
  state.alu_cmp <> alu.cmp
  state.ctrl_misalign <> bufreg.lsb(1)
  state.sh_done <> bufreg2.sh_done
  state.mem_misalign <> mem_if.misalign
  state.bne_or_bge <> decode.bne_or_bge
  state.cond_branch <> decode.cond_branch
  state.dbus_en <> decode.dbus_en
  state.two_stage_op <> decode.two_stage_op
  state.branch_op <> decode.branch_op
  state.shift_op <> decode.shift_op
  state.sh_right <> decode.sh_right
  state.alu_rd_sel1 <> decode.alu_rd_sel(1)
  state.rd_alu_en <> decode.rd_alu_en
  state.e_op <> decode.e_op
  state.rd_op <> decode.rd_op
  state.dbus_ack <> dbus_ack
  state.ibus_ack <> ibus_ack
  state.rf_ready <> rf_ready
  dbus_cyc <> state.dbus_cyc
  ibus_cyc <> state.ibus_cyc
  rf_rreq <> state.rf_rreq
  rf_wreq <> state.rf_wreq

  decode.wb_rdt <> ibus_rdt
  decode.wb_en <> ibus_ack

  immdec.cnt_en <> state.cnt_en
  immdec.cnt_done <> state.cnt_done
  immdec.immdec_en <> decode.immdec_en
  immdec.csr_imm_en <> decode.csr_imm_en
  immdec.ctrl <> decode.immdec_ctrl
  immdec.wb_en <> ibus_ack
  immdec.wb_rdt <> ibus_rdt

  bufreg.cnt0 <> state.cnt0
  bufreg.cnt1 <> state.cnt1
  bufreg.en <> state.bufreg_en
  bufreg.init_stage <> state.init_stage
  bufreg.rs1_en <> decode.bufreg_rs1_en
  bufreg.imm_en <> decode.bufreg_imm_en
  bufreg.clr_lsb <> decode.bufreg_clr_lsb
  bufreg.sh_signed <> decode.bufreg_sh_signed
  bufreg.rs1 <> rf_if.rs1
  bufreg.imm <> immdec.imm
  dbus_adr <> bufreg.dbus_adr

  bufreg2.en <> state.cnt_en
  bufreg2.init_stage <> state.init_stage
  bufreg2.cnt7 <> state.cnt7
  bufreg2.cnt_done <> state.cnt_done
  bufreg2.sh_right <> decode.sh_right
  bufreg2.lsb <> bufreg.lsb
  bufreg2.bytecnt <> state.mem_bytecnt
  bufreg2.op_b_sel <> decode.op_b_source
  bufreg2.shift_op <> decode.shift_op
  bufreg2.rs2 <> rf_if.rs2
  bufreg2.imm <> immdec.imm
  bufreg2.load <> dbus_ack
  bufreg2.dat_in <> dbus_rdt
  dbus_dat <> bufreg2.dat

  ctrl.wb_rst <> wb_rst
  ctrl.pc_en <> state.ctrl_pc_en
  ctrl.cnt12to31 <> state.cnt12to31
  ctrl.cnt0 <> state.cnt0
  ctrl.cnt1 <> state.cnt1
  ctrl.cnt2 <> state.cnt2
  ctrl.jump <> state.ctrl_jump
  ctrl.jal_or_jalr <> decode.ctrl_jal_or_jalr
  ctrl.utype <> decode.ctrl_utype
  ctrl.pc_rel <> decode.ctrl_pc_rel
  ctrl.trap <> (state.ctrl_trap || decode.ctrl_mret)
  ctrl.imm <> immdec.imm
  ctrl.bufreg_q <> bufreg.q
  ctrl.csr_pc <> rf_if.csr_pc
  ibus_adr <> ctrl.ibus_adr

  alu.en <> state.cnt_en
  alu.cnt0 <> state.cnt0
  alu.sub <> decode.alu_sub
  alu.bool_op <> decode.alu_bool_op
  alu.cmp_eq <> decode.alu_cmp_eq
  alu.cmp_sig <> decode.alu_cmp_sig
  alu.rd_sel <> decode.alu_rd_sel
  alu.rs1 <> rf_if.rs1
  alu.op_b <> bufreg2.op_b
  alu.bufreg_q <> bufreg.q

  rf_if.cnt_en <> state.cnt_en
  rf_if.rdata0 <> rdata0
  rf_if.rdata1 <> rdata1
  rf_if.trap <> state.ctrl_trap
  rf_if.mret <> decode.ctrl_mret
  rf_if.mepc <> ctrl.ibus_adr(0)
  rf_if.mtval_pc <> decode.mtval_pc
  rf_if.bufreg_q <> bufreg.q
  rf_if.bad_pc <> ctrl.bad_pc
  rf_if.csr_en <> decode.csr_en
  rf_if.csr_addr <> decode.csr_addr
  rf_if.csr_in <> csr.csr_in
  rf_if.rd_wen <> state.rf_rd_en
  rf_if.rd_waddr <> immdec.rd_addr
  rf_if.ctrl_rd <> ctrl.rd
  rf_if.alu_rd <> alu.rd
  rf_if.rd_alu_en <> decode.rd_alu_en
  rf_if.csr_rd <> csr.rd
  rf_if.rd_csr_en <> decode.rd_csr_en
  rf_if.mem_rd <> mem_if.rd
  rf_if.rd_mem_en <> decode.rd_mem_en
  rf_if.rs1_raddr <> immdec.rs1_addr
  rf_if.rs2_raddr <> immdec.rs2_addr
  wreg0 <> rf_if.wreg0
  wreg1 <> rf_if.wreg1
  wen0 <> rf_if.wen0
  wen1 <> rf_if.wen1
  wdata0 <> rf_if.wdata0
  wdata1 <> rf_if.wdata1
  rreg0 <> rf_if.rreg0
  rreg1 <> rf_if.rreg1

  mem_if.bytecnt <> state.mem_bytecnt
  mem_if.lsb <> bufreg.lsb
  mem_if.mem_signed <> decode.mem_signed
  mem_if.mem_word <> decode.mem_word
  mem_if.mem_half <> decode.mem_half
  mem_if.bufreg2_q <> bufreg2.q
  dbus_sel <> mem_if.wb_sel
  dbus_we <> decode.mem_cmd

  csr.wb_rst <> wb_rst
  csr.trig_irq <> ibus_ack
  csr.en <> state.cnt_en
  csr.cnt0to3 <> state.cnt0to3
  csr.cnt3 <> state.cnt3
  csr.cnt7 <> state.cnt7
  csr.cnt11 <> state.cnt11
  csr.cnt12 <> state.cnt12
  csr.cnt_done <> state.cnt_done
  csr.mem_op <> !decode.mtval_pc
  csr.mtip <> timer_irq
  csr.trap <> state.ctrl_trap
  csr.e_op <> decode.e_op
  csr.ebreak <> decode.ebreak
  csr.mem_cmd <> decode.mem_cmd
  csr.mstatus_en <> decode.csr_mstatus_en
  csr.mie_en <> decode.csr_mie_en
  csr.mcause_en <> decode.csr_mcause_en
  csr.csr_source <> decode.csr_source
  csr.mret <> decode.ctrl_mret
  csr.csr_d_sel <> decode.csr_d_sel
  csr.rf_csr_out <> rf_if.csr
  csr.csr_imm <> immdec.csr_imm
  csr.rs1 <> rf_if.rs1
end serv_top
