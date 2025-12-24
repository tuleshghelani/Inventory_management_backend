-- Migration script to split powder_coating_process into powder_coating_process and powder_coating_process_items
-- This script migrates from one-to-one to one-to-many relationship

-- Step 1: Create the new powder_coating_process_items table
CREATE TABLE IF NOT EXISTS public.powder_coating_process_items (
    id BIGSERIAL NOT NULL,
    powder_coating_process_id BIGINT NOT NULL,
    product_id BIGINT NULL,
    quantity INT4 NOT NULL,
    remaining_quantity INT4 NOT NULL,
    total_bags INT4 NULL,
    unit_price NUMERIC(10, 2) NULL,
    total_amount NUMERIC(12, 2) NULL,
    remarks VARCHAR NULL,
    client_id BIGINT NULL,
    CONSTRAINT powder_coating_process_items_pkey PRIMARY KEY (id),
    CONSTRAINT fk_pcpi_process_id_powder_coating_process_id 
        FOREIGN KEY (powder_coating_process_id) 
        REFERENCES public.powder_coating_process(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_powder_coating_process_items_product_id_product_id 
        FOREIGN KEY (product_id) 
        REFERENCES public.product(id),
    CONSTRAINT fk_powder_coating_process_items_client_id_client_id 
        FOREIGN KEY (client_id) 
        REFERENCES public.client(id)
);

-- Step 2: Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_pcpi_process_id ON public.powder_coating_process_items(powder_coating_process_id);
CREATE INDEX IF NOT EXISTS idx_pcpi_product_id ON public.powder_coating_process_items(product_id);
CREATE INDEX IF NOT EXISTS idx_pcpi_client_id ON public.powder_coating_process_items(client_id);

-- Step 3: Migrate data from old powder_coating_process table to new powder_coating_process_items table
-- Only migrate rows where quantity is not null (indicating there was an item)
-- Migrate existing powder_coating_process references into the new table when empty
DO $$
BEGIN
    IF to_regclass('public.powder_coating_process') IS NOT NULL
       AND to_regclass('public.powder_coating_process_items') IS NOT NULL
       AND (SELECT COUNT(*) FROM public.powder_coating_process_items) = 0 THEN
        INSERT INTO public.powder_coating_process_items (
            powder_coating_process_id,
            product_id,
            quantity,
            remaining_quantity,
            total_bags,
            unit_price,
            total_amount,
            remarks,
            client_id
        )
        SELECT
            pcp.id AS powder_coating_process_id,
            pcp.product_id,
            COALESCE(pcp.quantity, 0) AS quantity,
            COALESCE(pcp.remaining_quantity, 0) AS remaining_quantity,
            pcp.total_bags,
            pcp.unit_price,
            pcp.total_amount,
            pcp.remarks,
            pcp.client_id
        FROM public.powder_coating_process pcp
        WHERE pcp.quantity IS NOT NULL OR pcp.product_id IS NOT NULL;
    END IF;
END
$$;

-- Step 4: Drop the old columns from powder_coating_process table
-- Drop foreign key constraint first
-- ALTER TABLE public.powder_coating_process 
--     DROP CONSTRAINT IF EXISTS fk_pcp_product_id_product_id;

-- Drop the columns that have been moved to powder_coating_process_items
-- ALTER TABLE public.powder_coating_process 
--     DROP COLUMN IF EXISTS quantity,
--     DROP COLUMN IF EXISTS remaining_quantity,
--     DROP COLUMN IF EXISTS total_bags,
--     DROP COLUMN IF EXISTS unit_price,
--     DROP COLUMN IF EXISTS total_amount,
--     DROP COLUMN IF EXISTS remarks,
--     DROP COLUMN IF EXISTS product_id;

-- Note: client_id is kept in both tables as it may be needed at the process level
-- If you want to remove client_id from powder_coating_process, uncomment the line below:
-- ALTER TABLE public.powder_coating_process DROP COLUMN IF EXISTS client_id;

