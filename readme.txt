
S(ilent)Drive is a CP/M disk drive emulator

Can be run with JAVA 11 and above.

Implements CP/M disk geometry and commands:
home
track
read
write

Directory sectors are marked green. File area sectors are pale red when not in used and dark red when occupied.

- Stores files on host file system. Supports injecting files from input directory
- Displays drive allocation map, directory listing with file sizes
- Contents of DEFAULT directory is injected into CP/M disk on start
- FORMAt clears all contents of CP/M disk
- IMPORT does a one time import of files in IN directory
- Auto import does periodic import of files in IN directory
- DELETE deletes selected files from CP/M disk
- EXPORT saves all files from CP/M disk into host OUT directory

When contents of the CP/M disk is changed  independently from CP/M itself, the CP/M must be reinitialised to re-read disk directory.




Bellow are example BIOS routines:

;------------------------------------------------------------------------------
;
; S(ilent) Drive - communicates with virtual drive's host over MUART's serial port
;
;------------------------------------------------------------------------------
				;
                ; Command "R" - read sector
ReadSector:		lda		seek_disk		; get drive number
				ora		a				; is it first (virtual)dirve?
				jnz		read_flp		; no, it is floppy drive
				mvi		b,'R'			; command read
				call	SendCmd			; send it
				mvi		d,128			; counter
				lhld	DMAADR			; address to HL
ReadSector2:	call	ReadByte		; read byte
				mov		m,a				; store byte
				inx		h				; increment address
				dcr		d				; decrement counter
				jnz		ReadSector2		; loop
				xra		a				; flag success
				ret                     ; 
                ;
                ; Command "W" - write sector
WriteSector:	lda		seek_disk		; get drive number
				ora		a				; is it first (virtual)dirve?
				jnz		write_flp		; no, it is floppy drive
				mvi		b,'W'			; write command
				call	SendCmd			; send it
				mvi		d,128			; counter
				lhld	DMAADR			; address to HL
				mvi		b,0				; checksum
WriteSector2:	mov		a,m				; fetch byte from RAM
				mov		c,a				; move to C
				add		b				; compute checksum
				mov		b,a				; move to B
				call	SendByte		; send byte from C
				inx		h				; increment address
				dcr		d				; decrement counter
				jnz		WriteSector2	; loop
				jmp		SendChksum		; send checksum
				;
                ; Send track and sector number
SendSecTrk:     mvi		c,'T'			; track char
				call	SendByte		; send byte
				lda		SEKTRK			; track nr. to A
				call	SendByteA		; send byte
				mvi		c,'S'			; sector char
				call	SendByte		; send byte
				lda		SEKSEC			; sector nr. to A
				jmp		SendByteA		; send and return
				;
				; Send checksum
SendChksum:		mov		c,b				; get checksum
				call	SendByte		; send it
				call	ReadByte		; wait for acknowledge
				xri		'@'				; check success
				ret                     ; return
				;
                ; Read byte from MUART's serial port
ReadByte:	    in		MUART_ADDR+0Fh	; 8256 status
				ani		40h				; char received?
				jz		ReadByte		; no wait
				in		MUART_ADDR+07	; get the char
				ret
				;
				; Send byte from A (or C) over MUART's serial port
SendByteA:		mov		c,a				; move to C
SendByte:       in		MUART_ADDR+0Fh	; 8256 status
				ani		20h				; out buffer free ?
				jz		SendByte		; no wait
				mov		a,c				; char to A
				out		MUART_ADDR+07	; send
				ret
                ;
SendCmd:		mvi		c,':'			; start command sequence
				call	SendByte		; send start char
				mov		c,b				; get command
				call	SendByte		; send command
				jmp		SendSecTrk		; send rest
				;
				; for virtual drive just store, floppies need deblocking alg
SETSEC:			lda		seek_disk		; get drive number
				ora		a				; is it first (virtual)dirve?
				jnz		setsec_flp		; no, it is floppy drive
				mov		a,c				; get lower byte
				sta		seek_sector		; store sector
				ret
